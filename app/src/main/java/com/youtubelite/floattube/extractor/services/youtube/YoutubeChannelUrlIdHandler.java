package com.youtubelite.floattube.extractor.services.youtube;

import com.youtubelite.floattube.extractor.UrlIdHandler;
import com.youtubelite.floattube.extractor.exceptions.ParsingException;
import com.youtubelite.floattube.extractor.Parser;

/**
 * Created by Christian Schabesberger on 25.07.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * YoutubeChannelUrlIdHandler.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class YoutubeChannelUrlIdHandler implements UrlIdHandler {

    public String getUrl(String channelId) {
        return "https://www.youtube.com/" + channelId;
    }

    public String getId(String siteUrl) throws ParsingException {
        return Parser.matchGroup1("/(user/[A-Za-z0-9_-]*|channel/[A-Za-z0-9_-]*)", siteUrl);
    }

    public String cleanUrl(String siteUrl) throws ParsingException {
       return getUrl(getId(siteUrl));
    }

    public boolean acceptUrl(String videoUrl) {
        return (videoUrl.contains("youtube") ||
                videoUrl.contains("youtu.be")) &&
                ( videoUrl.contains("/user/") ||
                        videoUrl.contains("/channel/"));
    }
}
