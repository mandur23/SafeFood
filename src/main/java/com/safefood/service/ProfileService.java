package com.safefood.service;

import com.safefood.dao.ProfileDao;
import com.safefood.dto.PreferenceDto;

import java.util.List;
import java.util.Map;

public class ProfileService {
    private final ProfileDao profileDao;

    public ProfileService() {
        this.profileDao = new ProfileDao();
    }


    // Dao 메서드를 대신 호출
    public PreferenceDto getMyPreference(int userId) {
        return profileDao.getPreference(userId);
    }

    public List<String> getMyCategories(int userId) {
        return profileDao.getCategories(userId);
    }

    public Map<String, Integer> getMyAllergies(int userId) {
        return profileDao.getAllergies(userId);
    }
}
