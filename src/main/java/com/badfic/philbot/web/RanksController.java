package com.badfic.philbot.web;

import com.badfic.philbot.data.phil.Rank;
import com.github.mustachejava.Mustache;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RanksController extends BaseController {

    private Mustache mustache;

    @PostConstruct
    public void init() {
        mustache = mustacheFactory.compile("ranks.mustache");
    }

    @GetMapping(value = "/ranks", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> get(HttpSession httpSession) throws Exception {
        checkSession(httpSession, false);

        Map<String, Object> props = new HashMap<>();
        props.put("pageTitle", "Ranks");
        props.put("username", httpSession.getAttribute(DISCORD_USERNAME));
        props.put("ranks", Rank.getAllRanks());

        try (ReusableStringWriter stringWriter = ReusableStringWriter.getCurrent()) {
            mustache.execute(stringWriter, props);
            return ResponseEntity.ok(stringWriter.toString());
        }
    }
}
