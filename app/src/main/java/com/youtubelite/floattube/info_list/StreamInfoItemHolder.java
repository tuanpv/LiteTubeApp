package com.youtubelite.floattube.info_list;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.youtubelite.floattube.extractor.InfoItem;
import com.youtubelite.floattube.R;

/**
 * Created by Christian Schabesberger on 01.08.16.
 * <p>
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * StreamInfoItemHolder.java is part of NewPipe.
 * <p>
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class StreamInfoItemHolder extends InfoItemHolder {

    public final ImageView itemThumbnailView;
    public final TextView itemVideoTitleView,
            itemUploaderView,
            itemDurationView,
            itemUploadDateView,
            itemViewCountView;
    public final View itemRoot;

    public StreamInfoItemHolder(View v) {
        super(v);
        itemRoot = v.findViewById(R.id.itemRoot);
        itemThumbnailView = (ImageView) v.findViewById(R.id.itemThumbnailView);
        itemVideoTitleView = (TextView) v.findViewById(R.id.itemVideoTitleView);
        itemUploaderView = (TextView) v.findViewById(R.id.itemUploaderView);
        itemDurationView = (TextView) v.findViewById(R.id.itemDurationView);
        itemUploadDateView = (TextView) v.findViewById(R.id.itemUploadDateView);
        itemViewCountView = (TextView) v.findViewById(R.id.itemViewCountView);
    }

    @Override
    public InfoItem.InfoType infoType() {
        return InfoItem.InfoType.STREAM;
    }
}
