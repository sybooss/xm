package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper {
    @Select("select * from user_account where id=#{id}")
    UserAccount getById(Long id);

    @Select("select * from user_account where username=#{username}")
    UserAccount getByUsername(String username);

    @Insert("""
            insert into user_account(username, display_name, role, phone, status)
            values(#{username}, #{displayName}, #{role}, #{phone}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserAccount userAccount);
}
