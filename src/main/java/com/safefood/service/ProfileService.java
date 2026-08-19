package com.safefood.service;

import com.safefood.dao.ProfileDao;
import com.safefood.dto.PreferenceDto;
import com.safefood.view.DemoData;

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

    public void updatePreferencesOnly(int userId, int spicyLevel, int priceMax, int maxDistance, List<String> categories){
        profileDao.updatePreference(userId, spicyLevel, priceMax, maxDistance);
        
        profileDao.deleteCategory(userId);
        for (String cat : categories) {
            profileDao.insertCategory(userId, cat);
        }
    }

    public void updateAllergiesOnly(int userId, Map<String, Integer> allergies){
        profileDao.deleteAllergies(userId);
        for (Map.Entry<String, Integer> entry : allergies.entrySet()) {
            int allergyId = com.safefood.view.DemoData.ALLERGIES.indexOf(entry.getKey()) + 1;
            profileDao.insertAllergy(userId, allergyId, entry.getValue());
        }
    }
}
