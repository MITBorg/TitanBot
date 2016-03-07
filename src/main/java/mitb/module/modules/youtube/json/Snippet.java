package mitb.module.modules.youtube.json;

import java.util.ArrayList;
import java.util.List;

public final class Snippet {

    public String publishedAt;
    public String channelId;
    public String title;
    public String description;
    public Thumbnails thumbnails;
    public String channelTitle;
    public List<String> tags = new ArrayList<>();
    public String categoryId;
    public String liveBroadcastContent;
    public Localized localized;

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Thumbnails getThumbnails() {
        return thumbnails;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getLiveBroadcastContent() {
        return liveBroadcastContent;
    }

    public Localized getLocalized() {
        return localized;
    }
}
