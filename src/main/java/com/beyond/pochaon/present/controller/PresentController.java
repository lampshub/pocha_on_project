package com.beyond.pochaon.present.controller;

import com.beyond.pochaon.present.dto.PresentCreateDto;
import com.beyond.pochaon.present.service.PresentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/present")
public class PresentController {
    private final PresentService presentService;

    public PresentController(PresentService presentService) {
        this.presentService = presentService;
    }


    //    1. 1:1 선물하기
    @PostMapping("/send")
    public void sendPresent(@RequestBody PresentCreateDto createDto){
    presentService.sendPresent(createDto);
    }


}
