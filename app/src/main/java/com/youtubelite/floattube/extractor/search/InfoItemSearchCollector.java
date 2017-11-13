package com.youtubelite.floattube.extractor.search;

import com.youtubelite.floattube.extractor.InfoItemCollector;
import com.youtubelite.floattube.extractor.UrlIdHandler;
import com.youtubelite.floattube.extractor.exceptions.ExtractionException;
import com.youtubelite.floattube.extractor.exceptions.FoundAdException;
import com.youtubelite.floattube.extractor.stream_info.StreamInfoItemCollector;
import com.youtubelite.floattube.extractor.channel.ChannelInfoItemCollector;
import com.youtubelite.floattube.extractor.channel.ChannelInfoItemExtractor;
import com.youtubelite.floattube.extractor.stream_info.StreamInfoItemExtractor;

/**
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) Christian Schabesberger 2017 <chris.schabesberger@mailbox.org>
 * InfoItemSearchCollector.java is part of NewPipe.
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

public class InfoItemSearchCollector extends InfoItemCollector {
    private String suggestion = "";
    private StreamInfoItemCollector streamCollector;
    private ChannelInfoItemCollector channelCollector;

    SearchResult result = new SearchResult();

    InfoItemSearchCollector(UrlIdHandler handler, int serviceId) {
        super(serviceId);
        streamCollector = new StreamInfoItemCollector(handler, serviceId);
        channelCollector = new ChannelInfoItemCollector(serviceId);
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public SearchResult getSearchResult() throws ExtractionException {

        addFromCollector(channelCollector);
        addFromCollector(streamCollector);

        result.suggestion = suggestion;
        result.errors = getErrors();
        return result;
    }

    public void commit(StreamInfoItemExtractor extractor) {
        try {
            result.resultList.add(streamCollector.extract(extractor));
        } catch(FoundAdException ae) {
            System.err.println("Found add");
        } catch (Exception e) {
            addError(e);
        }
    }

    public void commit(ChannelInfoItemExtractor extractor) {
        try {
            result.resultList.add(channelCollector.extract(extractor));
        } catch(FoundAdException ae) {
            System.err.println("Found add");
        } catch (Exception e) {
            addError(e);
        }
    }
}
