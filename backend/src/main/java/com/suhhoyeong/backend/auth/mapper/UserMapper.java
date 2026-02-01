package com.suhhoyeong.backend.auth.mapper;

import com.suhhoyeong.backend.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
  User findByEmail(@Param("email") String email);

  int insertUser(User user);

  int updateTwofaSecretByEmail(@Param("email") String email,
      @Param("secret") String secret);

  int setTwofaEnabledByEmail(@Param("email") String email,
      @Param("enabled") boolean enabled);
}
