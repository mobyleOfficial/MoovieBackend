package com.moovie.backend.core.controller;

import com.moovie.backend.domain.usecase.GetCommentsUseCase;
import com.moovie.backend.domain.model.CommentListing;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
public class CommentsController {

    private final GetCommentsUseCase getCommentsUseCase;

    public CommentsController(GetCommentsUseCase getCommentsUseCase) {
        this.getCommentsUseCase = getCommentsUseCase;
    }

    @GetMapping("/{contentId}")
    public CommentListing comments(@PathVariable String contentId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        return getCommentsUseCase.invoke(contentId, page, pageSize);
    }
}
