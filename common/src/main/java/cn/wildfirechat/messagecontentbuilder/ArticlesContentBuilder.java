package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.proto.ProtoConstants;
import io.netty.util.internal.StringUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class ArticlesContentBuilder extends MessageContentBuilder{
    public static class Article {
        public String id;
        public String cover;
        public String title;
        public String url;
        public boolean readReport;
    }
    public Article topArticle;
    public List<Article> subArticles;

    public static ArticlesContentBuilder newBuilder(String id, String cover, String title, String url, boolean readReport) {
        ArticlesContentBuilder builder = new ArticlesContentBuilder();
        builder.topArticle = new Article();
        builder.topArticle.id = id;
        builder.topArticle.cover = cover;
        builder.topArticle.title = title;
        builder.topArticle.url = url;
        builder.topArticle.readReport = readReport;
        return builder;
    }

    public ArticlesContentBuilder addSubArticle(String id, String cover, String title, String url, boolean readReport) {
        Article article = new Article();
        article.id = id;
        article.cover = cover;
        article.title = title;
        article.url = url;
        article.readReport = readReport;
        if(subArticles == null) {
            subArticles = new ArrayList<>();
        }
        subArticles.add(article);
        return this;
    }

    private static JSONObject toJSON(Article article) {
        JSONObject top = new JSONObject();
        if(!StringUtil.isNullOrEmpty(article.id))
            top.put("id", article.id);
        if(!StringUtil.isNullOrEmpty(article.cover))
            top.put("cover", article.cover);
        if(!StringUtil.isNullOrEmpty(article.title))
            top.put("title", article.title);
        if(!StringUtil.isNullOrEmpty(article.url))
            top.put("url", article.url);
        if(article.readReport)
            top.put("rr", article.readReport);
        return top;
    }

    @Override
    public MessagePayload build() {
        MessagePayload payload = encodeBase();
        payload.setType(ProtoConstants.ContentType.Articles);
        payload.setPersistFlag(ProtoConstants.PersistFlag.Persist_And_Count);
        payload.setSearchableContent(topArticle.title);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("top", toJSON(topArticle));
        if(subArticles != null && !subArticles.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            subArticles.forEach(article -> jsonArray.add(toJSON(article)));
            jsonObject.put("subArticles", jsonArray);
        }

        payload.setBase64edData(Base64.getEncoder().encodeToString(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8)));
        return payload;
    }
}
