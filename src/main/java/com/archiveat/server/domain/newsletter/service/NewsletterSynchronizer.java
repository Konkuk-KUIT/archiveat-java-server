package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.NewsletterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NewsletterSynchronizer {

    private final DomainRepository domainRepository;
    private final NewsletterRepository newsletterRepository;

    /**
     * Domain 조회 또는 생성 (별도 트랜잭션)
     * 이미 존재하거나 동시에 생성되어 충돌 발생 시, 존재하는 Domain을 반환
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Domain getOrCreateDomain(String domainName) {
        return domainRepository.findByName(domainName)
                .orElseGet(() -> {
                    try {
                        return domainRepository.save(Domain.builder().name(domainName).build());
                    } catch (DataIntegrityViolationException e) {
                        return domainRepository.findByName(domainName)
                                .orElseThrow(() -> e);
                    }
                });
    }

    /**
     * Pending 상태의 Newsletter 조회 또는 생성 (별도 트랜잭션)
     * 이미 존재하거나 동시에 생성되어 충돌 발생 시, 존재하는 Newsletter 반환
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Newsletter getOrCreatePendingNewsletter(Domain domain, String contentUrl) {
        return newsletterRepository.findByContentUrl(contentUrl)
                .orElseGet(() -> {
                    try {
                        return newsletterRepository.save(Newsletter.createPending(domain, contentUrl));
                    } catch (DataIntegrityViolationException e) {
                        return newsletterRepository.findByContentUrl(contentUrl)
                                .orElseThrow(() -> e);
                    }
                });
    }
}
