package com.safefood.service;

import java.util.List;
import com.safefood.dao.OnboardingDao;

public class OnboardingService {
    private final OnboardingDao onboardingDao = new OnboardingDao();

    public void saveAllergy(int userId, int allergyId, int severity) {
        onboardingDao.insertAllergy(userId, allergyId, severity);
    }

    public void savePreferences(int userId, int spicyLevel, int priceMax, int
            maxDistance, List<String> categories) {
        // 단일 데이터인 취향 테이블 저장
        onboardingDao.insertPreference(userId, spicyLevel, priceMax, maxDistance);

        // 다중 데이터인 카테고리는 반복문을 돌며 하나씩 저장
        for (String category : categories) {
            onboardingDao.insertCategory(userId, category);
        }
    }
}
