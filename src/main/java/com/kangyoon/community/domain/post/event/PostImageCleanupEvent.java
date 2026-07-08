package com.kangyoon.community.domain.post.event;

import java.util.Set;

public record PostImageCleanupEvent (Set<String> imageUrls){
}
