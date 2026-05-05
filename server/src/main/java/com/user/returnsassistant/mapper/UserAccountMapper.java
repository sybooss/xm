package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.UserAccount;

public interface UserAccountMapper {
    UserAccount getById(Long id);

    UserAccount getByUsername(String username);

    void insert(UserAccount userAccount);
}
