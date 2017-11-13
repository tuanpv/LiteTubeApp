package com.youtubelite.floattube.extractor.playlist;

import com.youtubelite.floattube.extractor.exceptions.ParsingException;

public interface PlayListInfoItemExtractor {
    String getThumbnailUrl() throws ParsingException;
    String getPlayListName() throws ParsingException;
    String getWebPageUrl() throws ParsingException;
}
