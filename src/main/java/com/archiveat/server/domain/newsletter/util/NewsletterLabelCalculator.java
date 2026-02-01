package com.archiveat.server.domain.newsletter.util;

/**
 * Newsletter Label 계산 유틸리티
 * 
 * Label은 4가지로 구성:
 * 1. 영감수집 : Light + Now
 * 2. 집중탐구 : Deep + Now
 * 3. 성장한입 : Light + Future
 * 4. 관점확장 : Deep + Future
 */
public class NewsletterLabelCalculator {

    private static final int LIGHT_DEEP_THRESHOLD_MIN = 10; // 10분 이하 Light, 10분 이상 Deep

    /**
     * Newsletter의 label을 계산
     * 
     * @param consumptionTimeMin     Newsletter 소비 시간 (분)
     * @param newsletterCategory     Newsletter의 카테고리
     * @param userInterestCategories 사용자가 온보딩에서 선택한 관심사 카테고리 리스트
     * @return label 문자열 ("영감수집", "집중탐구", "성장한입", "관점확장")
     */
    public static String calculateLabel(
            Integer consumptionTimeMin,
            String newsletterCategory,
            java.util.List<String> userInterestCategories) {
        // 소비 시간이 없으면 label 계산 불가
        if (consumptionTimeMin == null) {
            return null;
        }

        // Light/Deep 결정 (10분 기준)
        boolean isLight = consumptionTimeMin < LIGHT_DEEP_THRESHOLD_MIN;

        // Now/Future 결정 (사용자 관심사 카테고리 포함 여부)
        boolean isNow = userInterestCategories != null
                && newsletterCategory != null
                && userInterestCategories.contains(newsletterCategory);

        // Label 조합
        if (isLight && isNow) {
            return "영감수집"; // Light + Now
        } else if (!isLight && isNow) {
            return "집중탐구"; // Deep + Now
        } else if (isLight && !isNow) {
            return "성장한입"; // Light + Future
        } else {
            return "관점확장"; // Deep + Future
        }
    }
}
