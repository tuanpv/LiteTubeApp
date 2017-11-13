package com.youtubelite.floattube.info_list;

import android.view.View;
import android.widget.TextView;

import com.youtubelite.floattube.R;
import com.youtubelite.floattube.extractor.InfoItem;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * ChannelInfoItemHolder .java is part of NewPipe.
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

public class ChannelInfoItemHolder extends InfoItemHolder {
    public final CircleImageView itemThumbnailView;
    public final TextView itemChannelTitleView;
    public final TextView itemSubscriberCountView;
    public final TextView itemVideoCountView;
    public final TextView itemChannelDescriptionView;

    public final View itemRoot;

    ChannelInfoItemHolder(View v) {
        super(v);
        itemRoot = v.findViewById(R.id.itemRoot);
        itemThumbnailView = (CircleImageView) v.findViewById(R.id.itemThumbnailView);
        itemChannelTitleView = (TextView) v.findViewById(R.id.itemChannelTitleView);
        itemSubscriberCountView = (TextView) v.findViewById(R.id.itemSubscriberCountView);
        itemVideoCountView = (TextView) v.findViewById(R.id.itemVideoCountView);
        itemChannelDescriptionView = (TextView) v.findViewById(R.id.itemChannelDescriptionView);
    }

    @Override
    public InfoItem.InfoType infoType() {
        return InfoItem.InfoType.CHANNEL;
    }
}
