package com.archiveat.server.domain.collection.service;

import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.explore.repository.UserTopicRepository;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.archiveat.server.global.exception.CustomException;
import com.archiveat.server.global.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionGeneratorService {

    private final UserRepository userRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final CollectionRepository collectionRepository;
    // Removed CollectionNewsletterRepository dependency as cascade handles saving
    private final TopicRepository topicRepository;
    private final UserTopicRepository userTopicRepository;
    private final TransactionTemplate transactionTemplate;

    public void generateCollectionsForTime(LocalTime time) {
        log.info("Starting collection generation for time: {}", time);

        // 1. Determine Target DepthType
        // 06:00 -> LIGHT (Short), Others -> DEEP (Long)
        DepthType targetDepth;
        if (time.getHour() == 6) {
            targetDepth = DepthType.LIGHT;
        } else {
            targetDepth = DepthType.DEEP;
        }

        // 2. Iterate all users (Simple implementation for now)
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                transactionTemplate.execute(status -> {
                    generateForUser(user, targetDepth);
                    return null;
                });
            } catch (Exception e) {
                log.error("Failed to generate collection for user {}", user.getId(), e);
            }
        }

        log.info("Collection generation completed for time: {}", time);
    }

    private void generateForUser(User user, DepthType targetDepth) {
        // 1. Find candidates (Uncollected, Matching Depth)
        List<UserNewsletter> candidates = userNewsletterRepository.findUncollectedNewsletters(user.getId(),
                targetDepth);

        if (candidates.isEmpty()) {
            return;
        }

        // 2. Group by Topic Name
        Map<String, List<UserNewsletter>> groupedByTopic = candidates.stream()
                .filter(un -> un.getNewsletter().getTopic() != null)
                .collect(Collectors.groupingBy(un -> un.getNewsletter().getTopic()));

        // 3. Build Clusters
        List<List<UserNewsletter>> validClusters = new ArrayList<>();

        for (Map.Entry<String, List<UserNewsletter>> entry : groupedByTopic.entrySet()) {
            List<UserNewsletter> items = entry.getValue();
            // Minimum 2 items
            if (items.size() < 2)
                continue;

            // Partition into chunks (Max 4)
            // Strategy: 10 -> 4, 4, 2
            int size = items.size();
            for (int i = 0; i < size; i += 4) {
                int end = Math.min(size, i + 4);
                int count = end - i;

                // If remaining is less than 2, try to merge with previous or discard?
                // Logic: "묶을 수 있을만큼 많이"
                // If we have 5: 4, 1 -> 1 is discarded? Or 3, 2?
                // Simple implementation: chunks of 4. If last chunk < 2, discard or add to
                // previous?
                // Requirement: Min 2, Max 4.
                // 5 items -> {1,2,3,4}, {5} (invalid) -> Just {1,2,3,4}
                // 6 items -> {1,2,3,4}, {5,6} (valid)
                if (count >= 2) {
                    validClusters.add(new ArrayList<>(items.subList(i, end)));
                } else {
                    // Less than 2 remaining.
                    // If we have a previous cluster from this topic, maybe steal one?
                    // E.g. 5 items -> 4, 1. If we make 3, 2 -> both valid.
                    // Advanced Logic: Distribution.
                    // For now, let's stick to simple chunks. If < 2, ignore.
                }
            }
        }

        if (validClusters.isEmpty()) {
            return;
        }

        // 4. Select ONE random cluster
        Collections.shuffle(validClusters);
        List<UserNewsletter> selectedCluster = validClusters.get(0);
        String topicName = selectedCluster.get(0).getNewsletter().getTopic();

        // 5. Determine PerspectiveType
        PerspectiveType perspectiveType = calculatePerspectiveType(user.getId(), topicName);

        // 6. Handle Area Constraint (Max 1 per Area)
        // Check if collection exists for (User, Depth, Perspective)
        // Handles potential duplicates by deleting all found collections
        List<Collection> existingCollections = collectionRepository.findByUserIdAndDepthTypeAndPerspectiveType(
                user.getId(), targetDepth, perspectiveType);

        if (!existingCollections.isEmpty()) {
            collectionRepository.deleteAll(existingCollections);
            collectionRepository.flush(); // Force DELETE execution
            log.info("Deleted {} existing collection(s) for user {} area {}/{}",
                    existingCollections.size(), user.getId(), targetDepth, perspectiveType);
        }

        // 7. Create Collection
        Topic topic = topicRepository.findByName(topicName)
                .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

        Collection collection = Collection.builder()
                .user(user)
                .topic(topic)
                .title(topicName + " 모음집") // Simple title for now
                .perspectiveType(perspectiveType)
                .depthType(targetDepth)
                .build();

        collectionRepository.save(collection);

        // 8. Save Collection (Cascade saves CollectionNewsletters)
        List<CollectionNewsletter> collectionNewsletters = selectedCluster.stream()
                .map(un -> CollectionNewsletter.builder()
                        .collection(collection)
                        .newsletter(un.getNewsletter())
                        .build())
                .collect(Collectors.toList());

        collection.getCollectionNewsletters().addAll(collectionNewsletters);
        collectionRepository.save(collection);
        log.info("Generated collection {} for user {}", collection.getId(), user.getId());
    }

    private PerspectiveType calculatePerspectiveType(Long userId, String topicName) {
        List<String> nowTopics = userTopicRepository.findTopicNamesByUserIdAndPerspectiveType(userId,
                PerspectiveType.NOW);
        return nowTopics.contains(topicName) ? PerspectiveType.NOW : PerspectiveType.FUTURE;
    }
}
