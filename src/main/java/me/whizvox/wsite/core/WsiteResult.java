package me.whizvox.wsite.core;

public enum WsiteResult {

  SUCCESS,
  USER_INVALID_USERNAME,
  USER_INVALID_EMAIL_ADDRESS,
  USER_INVALID_PASSWORD,
  USER_USERNAME_CONFLICT,
  USER_EMAIL_ADDRESS_CONFLICT,
  USER_ID_NOT_FOUND,
  USER_USERNAME_NOT_CHANGED,
  USER_EMAIL_ADDRESS_NOT_CHANGED,
  USER_PASSWORD_NOT_CHANGED,
  USER_OPERATOR_NOT_CHANGED,
  USER_INVALID_QUERY,
  USER_MATCHING_IDS,
  PAGE_INVALID_PATH,
  PAGE_PATH_CONFLICT,
  PAGE_PATH_NOT_FOUND,
  LOGIN_INVALID_QUERY,
  LOGIN_QUERY_NOT_FOUND,
  LOGIN_INCORRECT_PASSWORD,
  LOGIN_INVALID_EXPIRATION,
  LOGIN_INVALID_IP_ADDRESS,
  LOGIN_TOKEN_NOT_FOUND

}