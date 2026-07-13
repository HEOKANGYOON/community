package com.kangyoon.community.domain.post.event;

import java.util.Set;

public record PostImageMoveEvent(Set<String> tempUrls) {
}
