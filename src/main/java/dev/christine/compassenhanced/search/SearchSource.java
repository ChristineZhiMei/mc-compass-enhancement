package dev.christine.compassenhanced.search;

public enum SearchSource {
    BLOCK("message.compass_enhanced.source.block"),
    DROPPED_ITEM("message.compass_enhanced.source.dropped_item"),
    CONTAINER("message.compass_enhanced.source.container");

    private final String translationKey;

    SearchSource(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
