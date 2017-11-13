package com.youtubelite.floattube.extractor.services.youtube;

import com.youtubelite.floattube.extractor.StreamingService;
import com.youtubelite.floattube.extractor.UrlIdHandler;
import com.youtubelite.floattube.extractor.exceptions.ExtractionException;
import com.youtubelite.floattube.extractor.search.SearchEngine;
import com.youtubelite.floattube.extractor.channel.ChannelExtractor;
import com.youtubelite.floattube.extractor.playlist.PlayListExtractor;
import com.youtubelite.floattube.extractor.SuggestionExtractor;
import com.youtubelite.floattube.extractor.stream_info.StreamExtractor;

import java.io.IOException;


/**
 * Created by Christian Schabesberger on 23.08.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * YoutubeService.java is part of NewPipe.
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

public class YoutubeService extends StreamingService {

    public YoutubeService(int id) {
        super(id);
    }

    @Override
    public ServiceInfo getServiceInfo() {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.name = "Youtube";
        return serviceInfo;
    }
    @Override
    public StreamExtractor getExtractorInstance(String url)
            throws ExtractionException, IOException {
        UrlIdHandler urlIdHandler = YoutubeStreamUrlIdHandler.getInstance();
        if(urlIdHandler.acceptUrl(url)) {
            return new YoutubeStreamExtractor(urlIdHandler, url, getServiceId());
        }
        else {
            throw new IllegalArgumentException("supplied String is not a valid Youtube URL");
        }
    }
    @Override
    public SearchEngine getSearchEngineInstance() {
        return new YoutubeSearchEngine(getStreamUrlIdHandlerInstance(), getServiceId());
    }

    @Override
    public UrlIdHandler getStreamUrlIdHandlerInstance() {
        return YoutubeStreamUrlIdHandler.getInstance();
    }

    @Override
    public UrlIdHandler getChannelUrlIdHandlerInstance() {
        return new YoutubeChannelUrlIdHandler();
    }


    @Override
    public UrlIdHandler getPlayListUrlIdHandlerInstance() {
        return new YoutubePlayListUrlIdHandler();
    }

    @Override
    public ChannelExtractor getChannelExtractorInstance(String url, int page)
        throws ExtractionException, IOException {
        return new YoutubeChannelExtractor(getChannelUrlIdHandlerInstance(), url, page, getServiceId());
    }

    public PlayListExtractor getPlayListExtractorInstance(String url, int page)
        throws ExtractionException, IOException {
        return new YoutubePlayListExtractor(getPlayListUrlIdHandlerInstance(), url, page, getServiceId());
    }

    @Override
    public SuggestionExtractor getSuggestionExtractorInstance() {
        return new YoutubeSuggestionExtractor(getServiceId());
    }
}
