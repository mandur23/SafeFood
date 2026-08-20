package com.safefood.service;

import com.safefood.dao.HistoryDao;
import com.safefood.dto.HistoryDto;
import java.util.List;

public class HistoryService {
    private final HistoryDao historyDao = new HistoryDao();

    // 저장
    public boolean saveEatenHistory(int userId, int menuId){
        return historyDao.insertHistory(userId, menuId, null, "EATEN");
    }

    // 꺼내기
    public List<HistoryDto> getEatenHistory(int userId){
        return historyDao.findHistoriesByUserId(userId);
    }

    public List<HistoryDto> getHistories(int userId) {
        return historyDao.findHistoriesByUserId(userId);
    }
}