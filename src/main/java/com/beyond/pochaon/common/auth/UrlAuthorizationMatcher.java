package com.beyond.pochaon.common.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UrlAuthorizationMatcher {

    private static final LinkedHashMap<String, List<TokenStage>> RULES = new LinkedHashMap<>();

    static {
        //  반드시 구체 경로 → 범용 경로 순서

        RULES.put("/owner/base", List.of(TokenStage.BASE));

        RULES.put("/store/select", List.of(TokenStage.BASE));
        RULES.put("/store/create", List.of(TokenStage.BASE));
        RULES.put("/store/list", List.of(TokenStage.BASE, TokenStage.STORE));
        RULES.put("/store/monthlysettlement", List.of(TokenStage.BASE, TokenStage.STORE));
        RULES.put("/store/dailysettlement", List.of(TokenStage.BASE, TokenStage.STORE));
        RULES.put("/store", List.of(TokenStage.STORE));

        RULES.put("/customertable/select", List.of(TokenStage.STORE));
        RULES.put("/customertable", List.of(TokenStage.STORE, TokenStage.BASE));

        RULES.put("/ordering", List.of(TokenStage.STORE));
        RULES.put("/chat", List.of(TokenStage.TABLE));

        RULES.put("/cart", List.of(TokenStage.TABLE));
        RULES.put("/orders", List.of(TokenStage.TABLE));


    }

    public static boolean isAllowed(String uri, TokenStage stage) {

        for (Map.Entry<String, List<TokenStage>> entry : RULES.entrySet()) {

            String pattern = entry.getKey();

            //  exact match 우선
            if (uri.equals(pattern)) {
                return entry.getValue().contains(stage);
            }

            //  prefix match
            if (uri.startsWith(pattern + "/")) {
                return entry.getValue().contains(stage);
            }
        }

        // 규칙 없는 URL → 통과
        return true;
    }
}
