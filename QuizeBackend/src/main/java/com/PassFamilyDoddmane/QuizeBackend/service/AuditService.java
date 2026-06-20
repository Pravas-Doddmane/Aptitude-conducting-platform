package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.enums.AuditAction;
import com.PassFamilyDoddmane.QuizeBackend.entity.AuditLog;
import com.PassFamilyDoddmane.QuizeBackend.entity.User;
import com.PassFamilyDoddmane.QuizeBackend.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public void logAction(AuditAction action, String entityType, String entityId, Object beforeState, Object afterState) {
        persist(currentUserService.getCurrentUser(), action, entityType, entityId, beforeState, afterState);
    }

    public void logSystemAction(AuditAction action, String entityType, String entityId, Object beforeState, Object afterState) {
        persist(null, action, entityType, entityId, beforeState, afterState);
    }

    private void persist(User actorUser,
                         AuditAction action,
                         String entityType,
                         String entityId,
                         Object beforeState,
                         Object afterState) {
        AuditLog log = new AuditLog();
        log.setActorUser(actorUser);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBeforeState(serialize(beforeState));
        log.setAfterState(serialize(afterState));
        auditLogRepository.save(log);
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
