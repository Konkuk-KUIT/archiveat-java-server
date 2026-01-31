package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.newsletter.dto.response.*;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.NewsletterRepository;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@Service
public class NewsletterService {
    private final NewsletterRepository newsletterRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final UserRepository userRepository;
    private final DomainRepository domainRepository;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Transactional
    public DeleteNewsletterResponse deleteUserNewsletter(Long userId, Long userNewsletterId) {
        int deleted = userNewsletterRepository.deleteByIdAndUser_Id(userNewsletterId, userId);
        if(deleted == 0) {
            // TODO throw new NewsletterNotFoundException
        }
        return new DeleteNewsletterResponse(userNewsletterId);
    }

    @Transactional
    public ViewNewsletterResponse viewUserNewsletter(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Newsletter not found or access denied"));

        if(!userNewsletter.isRead())
            userNewsletter.updateIsRead();
        else
            userNewsletter.updateLastViewedAt();
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();

        List<NewsletterSummaryBlock> summaryBlocks = List.of();

        return new ViewNewsletterResponse(
                userNewsletter.getId(),                 // userNewsletterId
                null,                                   // categoryName (추후 연결)
                null,                                   // topicName (추후 연결)
                newsletter.getTitle(),
                newsletter.getThumbnailUrl(),
                null,                                   // label (아직 도메인 없음)
                userNewsletter.getMemo(),
                newsletter.getContentUrl(),
                summaryBlocks
        );
    }

    @Transactional
    public SimpleViewNewsletterResponse simpleViewUserNewsletter(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Newsletter not found or access denied"));

        if(!userNewsletter.isRead())
            userNewsletter.updateIsRead();
        else
            userNewsletter.updateLastViewedAt();
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();

        List<NewsletterSummaryBlock> summaryBlocks = List.of();

        return new SimpleViewNewsletterResponse(
                userNewsletter.getId(),                 // userNewsletterId
                null,                                   // categoryName (추후 연결)
                null,                                   // topicName (추후 연결)
                newsletter.getTitle(),
                newsletter.getThumbnailUrl(),
                null,                                   // label (아직 도메인 없음)
                userNewsletter.getMemo(),
                newsletter.getContentUrl(),
                summaryBlocks
        );
    }

    @Transactional
    public GenerateNewsletterResponse generateNewsletter(Long userId, String contentUrl, String memo) {
        Domain domain = resolveDomainFromUrl(contentUrl);

        User user =  userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User Not Found"));

        Newsletter newsletter = newsletterRepository.findByContentUrl(contentUrl)
                .orElseGet(()-> newsletterRepository.save(Newsletter.createPending(domain, contentUrl)));

        UserNewsletter userNewsletter = userNewsletterRepository.save(
                UserNewsletter.create(user, newsletter, memo)
        );

        // 커밋 이후에 LLM 작업 시작시키기(중요)
//        applicationEventPublisher.publishEvent(
//                new NewsletterLlmRequestedEvent(newsletter.getId(), contentUrl)
//        );

        return new GenerateNewsletterResponse(
                // TODO userNewsletterId 인지 newsletterId인지
                userNewsletter.getId(),
                newsletter.getLlmStatus().name()
        );
    }

    @Transactional
    public void updateIsRead(Long userId, Long userNewsletterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User Not Found"));

        UserNewsletter userNewsletter = userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(()-> new IllegalArgumentException("Newsletter not found or access denied"));

        userNewsletter.updateIsRead();
    }

    private Domain getOrCreateDomain(String domainName) {
        return domainRepository.findByName(domainName)
                .orElseGet(() -> {
                    try {
                        return domainRepository.save(new Domain(domainName));
                    } catch (DataIntegrityViolationException e) {
                        // 동시성 조절: 동시에 다른 트랜잭션에서 생성한 경우
                        return domainRepository.findByName(domainName)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private Domain resolveDomainFromUrl(String contentUrl) {
        String host = extractDomainName(contentUrl);
        String domainName = normalizeDomainName(host);
        return getOrCreateDomain(domainName);
    }

    public String extractDomainName(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost(); // www.youtube.com
            if (host == null) return null;

            // www 제거
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host; // youtube.com
        } catch (Exception e) {
            return null;
        }
    }

    public String normalizeDomainName(String host) {
        if (host == null) return "Unknown";

        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            return "YouTube";
        }
        if (host.contains("news.naver.com")) {
            return "Naver News";
        }
        if (host.contains("brunch.co.kr")) {
            return "Brunch";
        }
        if (host.contains("news.naver.com")) {
            return "Naver News";
        }
        if(host.contains("tistory.com")) {
            return "tistory";
        }
        return host; // fallback
    }
}
