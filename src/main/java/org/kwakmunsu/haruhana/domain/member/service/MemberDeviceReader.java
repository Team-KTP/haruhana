package org.kwakmunsu.haruhana.domain.member.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kwakmunsu.haruhana.domain.member.repository.MemberDeviceJpaRepository;
import org.kwakmunsu.haruhana.global.entity.EntityStatus;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MemberDeviceReader {

    private final MemberDeviceJpaRepository memberDeviceJpaRepository;

    public List<String> findDeviceTokensByMemberIds(List<Long> memberIds) {
        return memberDeviceJpaRepository.findDeviceTokensByMemberIdsAndStatus(memberIds, EntityStatus.ACTIVE);
    }

    public List<String> findAllActiveDeviceTokens() {
        return memberDeviceJpaRepository.findAllDeviceTokensByStatus(EntityStatus.ACTIVE);
    }

}