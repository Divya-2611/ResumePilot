package com.resumepilot.service;

import com.resumepilot.dto.request.ProfileUpdateRequest;
import com.resumepilot.dto.response.UserResponse;
import com.resumepilot.entity.Role;
import com.resumepilot.entity.User;
import com.resumepilot.exception.BadRequestException;
import com.resumepilot.exception.FileStorageException;
import com.resumepilot.exception.ResourceNotFoundException;
import com.resumepilot.mapper.UserMapper;
import com.resumepilot.repository.DownloadRecordRepository;
import com.resumepilot.repository.JobDescriptionRepository;
import com.resumepilot.repository.OptimizationHistoryRepository;
import com.resumepilot.repository.RefreshTokenRepository;
import com.resumepilot.repository.ResumeRepository;
import com.resumepilot.repository.RoleRepository;
import com.resumepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile management: read/update profile, avatar upload, account deletion.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FileStorageService fileStorageService;
    private final DownloadRecordRepository downloadRepository;
    private final OptimizationHistoryRepository historyRepository;
    private final JobDescriptionRepository jdRepository;
    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable, String search) {
        Page<User> users = (search == null || search.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.search(search.trim(), pageable);
        return users.map(UserMapper::toResponse);
    }

    @Transactional
    public UserResponse updateRole(Long actorId, Long targetId, String roleName) {
        if (actorId.equals(targetId)) {
            throw new BadRequestException("You cannot change your own role");
        }
        User user = getUser(targetId);
        Role.RoleName name = parseRole(roleName);
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", name));
        user.getRoles().clear();
        user.getRoles().add(role);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void deleteUserByAdmin(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        User user = getUser(targetId);
        deleteUserData(user);
        userRepository.delete(user);
    }

    private Role.RoleName parseRole(String roleName) {
        try {
            return Role.RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role must be USER or ADMIN");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserMapper.toResponse(getUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getUser(userId);

        // Email is immutable after registration; only names can be updated.
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    /** Deletes the account and all owned data. */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = getUser(userId);
        deleteUserData(user);
        userRepository.delete(user);
    }

    /** Removes every row that references the user before the account itself. */
    private void deleteUserData(User user) {
        Long userId = user.getId();
        fileStorageService.deleteUserDirectory(userId);
        refreshTokenRepository.deleteByUserId(userId);
        historyRepository.deleteByUserId(userId);
        downloadRepository.deleteByUserId(userId);
        jdRepository.deleteByUserId(userId);
        resumeRepository.deleteAll(resumeRepository.findByUserId(userId));
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
