package com.kangyoon.community.global.common;

public record ApiResponse<T> (String message, T data) { }
