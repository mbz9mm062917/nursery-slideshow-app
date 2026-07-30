package com.nursery.slideshow.photo.dto;

import java.util.List;

public record PhotoOrderRequest(List<Long> photoIds) {
}
