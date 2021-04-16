package sk.virtualvoid.nyxdroid.v2.data.dac;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sk.virtualvoid.core.ITaskQuery;
import sk.virtualvoid.core.NyxException;
import sk.virtualvoid.core.Task;
import sk.virtualvoid.core.TaskListener;
import sk.virtualvoid.core.TaskWorker;
import sk.virtualvoid.net.nyx.Connector;
import sk.virtualvoid.nyxdroid.library.Constants;
import sk.virtualvoid.nyxdroid.v2.data.Bookmark;
import sk.virtualvoid.nyxdroid.v2.data.BookmarkCategory;
import sk.virtualvoid.nyxdroid.v2.data.Context;
import sk.virtualvoid.nyxdroid.v2.data.SuccessResponse;
import sk.virtualvoid.nyxdroid.v2.data.query.BookmarkQuery;

import android.app.Activity;

/**
 * @author Juraj
 */
public class BookmarkDataAccess {
    private final static Logger log = Logger.getLogger(BookmarkDataAccess.class);

    public static Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> getBookmarks(Activity context, TaskListener<SuccessResponse<ArrayList<Bookmark>>> listener) {
        return new Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>>(context, new GetBookmarksTaskWorker(), listener);
    }

    public static Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> searchBookmarks(Activity context, TaskListener<SuccessResponse<ArrayList<Bookmark>>> listener) {
        return new Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>>(context, new SearchBookmarksTaskWorker(), listener);
    }

    public static Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> getMovement(Activity context, TaskListener<SuccessResponse<ArrayList<Bookmark>>> listener) {
        return new Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>>(context, new GetMovementTaskWorker(), listener);
    }

    public static Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> getInCategory(Activity context, TaskListener<SuccessResponse<ArrayList<Bookmark>>> listener) {
        return new Task<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>>(context, new GetBookmarksInCategoryTaskWorker(), listener);
    }

    public static Task<ITaskQuery, SuccessResponse<ArrayList<BookmarkCategory>>> getBookmarkCategories(Activity context, TaskListener<SuccessResponse<ArrayList<BookmarkCategory>>> listener) {
        return new Task<ITaskQuery, SuccessResponse<ArrayList<BookmarkCategory>>>(context, new GetBookmarkCategoriesTaskWorker(), listener);
    }

    private static Bookmark convert(JSONObject bookmark, JSONObject category) throws JSONException {
        Bookmark result = new Bookmark();
        result.Id = bookmark.getLong("discussion_id");
        if (category != null) {
            result.CategoryId = category.getLong("id");
        }
        result.Name = bookmark.getString("full_name");

        if (bookmark.has("new_posts_count")) {
            result.Unread = bookmark.getInt("new_posts_count");
        }

        // TODO: new_links_count, new_images_count
        // TODO: last_visited_at

        if (bookmark.has("new_replies_count") && !bookmark.isNull("new_replies_count")) {
            result.Replies = bookmark.getInt("new_replies_count");
        }

        return result;
    }

    private static BookmarkCategory convert(JSONObject category) throws JSONException {
        BookmarkCategory result = new BookmarkCategory();
        result.Id = category.getLong("id");
        result.Name = category.getString("category_name");
        return result;
    }

    public static class GetBookmarksTaskWorker extends TaskWorker<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> {
        @Override
        public SuccessResponse<ArrayList<Bookmark>> doWork(BookmarkQuery input) throws NyxException {
            ArrayList<Bookmark> resultList = new ArrayList<Bookmark>();
            Context context = null;

            Connector connector = new Connector(getContext());
            JSONObject json = connector.get("/bookmarks" + (input.IncludeUnread ? "/all" : ""));
            if (json == null) {
                throw new NyxException("Json result was null ?");
            } else {
                try {
                    JSONArray rootmarks = json.getJSONArray("bookmarks");
                    for (int rootMarkIndex = 0; rootMarkIndex < rootmarks.length(); rootMarkIndex++) {
                        JSONObject rootmark = rootmarks.getJSONObject(rootMarkIndex);

                        JSONObject category = rootmark.getJSONObject("category");
                        resultList.add(convert(category));

                        JSONArray bookmarksInCategory = rootmark.getJSONArray("bookmarks");
                        for (int bookmarkInCategoryIndex = 0; bookmarkInCategoryIndex < bookmarksInCategory.length(); bookmarkInCategoryIndex++) {
                            JSONObject bookmark = bookmarksInCategory.getJSONObject(bookmarkInCategoryIndex);
                            Bookmark result = convert(bookmark, category);
                            if (!input.IncludeUnread && result.Unread == 0) {
                                continue;
                            }
                            resultList.add(result);
                        }
                    }

                    context = Context.fromJSONObject(json);
                } catch (Throwable e) {
                    log.error("GetBookmarksTaskWorker", e);
                    throw new NyxException(e);
                }
            }

            return new SuccessResponse<>(resultList, context);
        }
    }

    public static class GetMovementTaskWorker extends TaskWorker<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> {
        @Override
        public SuccessResponse<ArrayList<Bookmark>> doWork(BookmarkQuery input) throws NyxException {
            ArrayList<Bookmark> resultList = new ArrayList<Bookmark>();
            Context context = null;

            Connector connector = new Connector(getContext());
            JSONObject json = connector.get("/bookmarks/history"); // TODO: /bookmarks/history/more ?
            if (json == null) {
                throw new NyxException("Json result was null ?");
            } else {
                try {
                    JSONArray discussions = json.getJSONArray("discussions");
                    for (int discussionIndex = 0; discussionIndex < discussions.length(); discussionIndex++) {
                        JSONObject discussion = discussions.getJSONObject(discussionIndex);
                        Bookmark result = convert(discussion, null);
                        if (!input.IncludeUnread && result.Unread == 0) {
                            continue;
                        }
                        resultList.add(result);
                    }

                    context = Context.fromJSONObject(json);
                } catch (Throwable e) {
                    log.error("GetBookmarksTaskWorker", e);
                    throw new NyxException(e);
                }
            }

            return new SuccessResponse<>(resultList, context);
        }
    }

    public static class SearchBookmarksTaskWorker extends TaskWorker<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> {
        @Override
        public SuccessResponse<ArrayList<Bookmark>> doWork(BookmarkQuery input) throws NyxException {
            throw new NyxException(Constants.NOT_IMPLEMENTED_YET);
        }
    }


    public static class GetBookmarkCategoriesTaskWorker extends TaskWorker<ITaskQuery, SuccessResponse<ArrayList<BookmarkCategory>>> {
        @Override
        public SuccessResponse<ArrayList<BookmarkCategory>> doWork(ITaskQuery input) throws NyxException {
            ArrayList<BookmarkCategory> resultList = new ArrayList<BookmarkCategory>();
            Context context = null;

            Connector connector = new Connector(getContext());
            JSONObject json = connector.get("/bookmarks/all");
            if (json == null) {
                throw new NyxException("Json result was null ?");
            } else {
                try {
                    JSONArray bookmarks = json.getJSONArray("bookmarks");
                    for (int bokmarkIndex = 0; bokmarkIndex < bookmarks.length(); bokmarkIndex++) {
                        JSONObject bookmark = bookmarks.getJSONObject(bokmarkIndex);

                        JSONObject category = bookmark.getJSONObject("category");
                        resultList.add(convert(category));
                    }

                    context = Context.fromJSONObject(json);
                } catch (Throwable e) {
                    log.error("GetBookmarksTaskWorker", e);
                    throw new NyxException(e);
                }
            }

            return new SuccessResponse<>(resultList, context);
        }
    }


    public static class GetBookmarksInCategoryTaskWorker extends TaskWorker<BookmarkQuery, SuccessResponse<ArrayList<Bookmark>>> {
        @Override
        public SuccessResponse<ArrayList<Bookmark>> doWork(BookmarkQuery input) throws NyxException {
            ArrayList<Bookmark> resultList = new ArrayList<Bookmark>();
            Context context = null;

            Connector connector = new Connector(getContext());
            JSONObject json = connector.get("/bookmarks/all");
            if (json == null) {
                throw new NyxException("Json result was null ?");
            } else {
                try {
                    JSONArray bookmarks = json.getJSONArray("bookmarks");
                    for (int bookmarkIndex = 0; bookmarkIndex < bookmarks.length(); bookmarkIndex++) {
                        JSONObject bookmark = bookmarks.getJSONObject(bookmarkIndex);
                        JSONObject category = bookmark.getJSONObject("category");

                        // skokotena skurvena vyjebana java do pice, nie == ako normalni ludia, ale EQUALS !!!
                        // skoro hodinu som tu stravil. boha krista jebem !!!
                        if (convert(category).Id.equals(input.CategoryId)) {
                            JSONArray bookmarksInCategory = bookmark.getJSONArray("bookmarks");
                            for (int bookmarkInCategoryIndex = 0; bookmarkInCategoryIndex < bookmarksInCategory.length(); bookmarkInCategoryIndex++) {
                                JSONObject bookmarkInCategory = bookmarksInCategory.getJSONObject(bookmarkInCategoryIndex);
                                resultList.add(convert(bookmarkInCategory, category));
                            }
                            break;
                        }
                    }

                    context = Context.fromJSONObject(json);
                } catch (Throwable e) {
                    log.error("GetBookmarksTaskWorker", e);
                    throw new NyxException(e);
                }
            }

            return new SuccessResponse<>(resultList, context);
        }
    }
}
