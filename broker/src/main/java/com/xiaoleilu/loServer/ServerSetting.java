package com.xiaoleilu.loServer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.action.DefaultIndexAction;
import com.xiaoleilu.loServer.action.UnknownErrorAction;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.exception.ServerSettingException;
import com.xiaoleilu.loServer.filter.Filter;
import org.slf4j.LoggerFactory;

/**
 * 全局设定文件
 * @author xiaoleilu
 *
 */
public class ServerSetting {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ServerSetting.class);
	
	//-------------------------------------------------------- Default value start
	/** 默认的字符集编码 */
	public final static String DEFAULT_CHARSET = "utf-8";
	
	public final static String MAPPING_ALL = "/*";
	
	public final static String MAPPING_ERROR = "/_error";
	//-------------------------------------------------------- Default value end
	
	/** 字符编码 */
	private static String charset = DEFAULT_CHARSET;
	/** 端口 */
	private static int port = 8090;
	/** 根目录 */
	private static File root;
	/** Filter映射表 */
	private static Map<String, Filter> filterMap = new ConcurrentHashMap<>();
	/** Action映射表 */
	private static Map<String, Class<? extends Action>> getActionMap = new ConcurrentHashMap<>();
    private static Map<String, Class<? extends Action>> postActionMap = new ConcurrentHashMap<>();
    private static Map<String, Class<? extends Action>> putActionMap = new ConcurrentHashMap<>();
    private static Map<String, Class<? extends Action>> deleteActionMap = new ConcurrentHashMap<>();
    private static Map<String, Class<? extends Action>> errorActionMap = new ConcurrentHashMap<>();
	
	static{
        errorActionMap.put(StrUtil.SLASH, DefaultIndexAction.class);
        errorActionMap.put(MAPPING_ERROR, UnknownErrorAction.class);
	}
	
	/**
	 * @return 获取编码
	 */
	public static String getCharset() {
		return charset;
	}
	/**
	 * @return 字符集
	 */
	public static Charset charset() {
		return Charset.forName(getCharset());
	}
	
	/**
	 * 设置编码
	 * @param charset 编码
	 */
	public static void setCharset(String charset) {
		ServerSetting.charset = charset;
	}
	
	/**
	 * @return 监听端口
	 */
	public static int getPort() {
		return port;
	}
	/**
	 * 设置监听端口
	 * @param port 端口
	 */
	public static void setPort(int port) {
		ServerSetting.port = port;
	}
	
	//----------------------------------------------------------------------------------------------- Root start
	/**
	 * @return 根目录
	 */
	public static File getRoot() {
		return root;
	}
	/**
	 * @return 根目录
	 */
	public static boolean isRootAvailable() {
		if(root != null && root.isDirectory() && root.isHidden() == false && root.canRead()){
			return true;
		}
		return false;
	}
	/**
	 * @return 根目录
	 */
	public static String getRootPath() {
		return FileUtil.getAbsolutePath(root);
	}
	/**
	 * 根目录
	 * @param root 根目录绝对路径
	 */
	public static void setRoot(String root) {
		ServerSetting.root = FileUtil.mkdir(root);
        Logger.debug("Set root to [{}]", ServerSetting.root.getAbsolutePath());
	}
	/**
	 * 根目录
	 * @param root 根目录绝对路径
	 */
	public static void setRoot(File root) {
		if(root.exists() == false){
			root.mkdirs();
		}else if(root.isDirectory() == false){
			throw new ServerSettingException(StrUtil.format("{} is not a directory!", root.getPath()));
		}
		ServerSetting.root = root;
	}
	//----------------------------------------------------------------------------------------------- Root end
	
	//----------------------------------------------------------------------------------------------- Filter start
	/**
	 * @return 获取FilterMap
	 */
	public static Map<String, Filter> getFilterMap() {
		return filterMap;
	}
	/**
	 * 获得路径对应的Filter
	 * @param path 路径，为空时将获得 根目录对应的Action
	 * @return Filter
	 */
	public static Filter getFilter(String path){
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
		return getFilterMap().get(path.trim());
	}
	/**
	 * 设置FilterMap
	 * @param filterMap FilterMap
	 */
	public static void setFilterMap(Map<String, Filter> filterMap) {
		ServerSetting.filterMap = filterMap;
	}
	
	/**
	 * 设置Filter类，已有的Filter类将被覆盖
	 * @param path 拦截路径（必须以"/"开头）
	 * @param filter Action类
	 */
	public static void setFilter(String path, Filter filter) {
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
		
		if(null == filter) {
			Logger.warn("Added blank action, pass it.");
			return;
		}
		//所有路径必须以 "/" 开头，如果没有则补全之
		if(false == path.startsWith(StrUtil.SLASH)) {
			path = StrUtil.SLASH + path;
		}
		
		ServerSetting.filterMap.put(path, filter);
	}
	
	/**
	 * 设置Filter类，已有的Filter类将被覆盖
	 * @param path 拦截路径（必须以"/"开头）
	 * @param filterClass Filter类
	 */
	public static void setFilter(String path, Class<? extends Filter> filterClass) {
		setFilter(path, (Filter) Singleton.get(filterClass));
	}
	//----------------------------------------------------------------------------------------------- Filter end
	
	//----------------------------------------------------------------------------------------------- Action start
	/**
	 * @return 获取ActionMap
	 */
	public static Map<String, Class<? extends Action>> getActionMap(String method) {
	    if (method.equals("GET")) {
            return getActionMap;
        } else if(method.equals("POST")) {
	        return postActionMap;
        } else if(method.equals("PUT")) {
	        return putActionMap;
        } else if(method.equals("DELETE")) {
	        return deleteActionMap;
        }
		return getActionMap;
	}

    public static Action getErrorAction(String path) {
        Class<? extends Action> cls = errorActionMap.get(path);
        if(cls != null) {
            try {
                return cls.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
	 * 获得路径对应的Action
	 * @param path 路径，为空时将获得 根目录对应的Action
	 * @return Action
	 */
	public static Action getAction(String path, String method){
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
        Class<? extends Action> cls = getActionMap(method).get(path.trim());
        if(cls != null) {
            try {
                return cls.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
	}
	
	/**
	 * 增加Action类，已有的Action类将被覆盖<br>
	 * 所有Action都是以单例模式存在的！
	 * @param path 拦截路径（必须以"/"开头）
	 * @param actionClass Action类
	 */
	public static void setAction(String path, Class<? extends Action> actionClass) {
        if(StrUtil.isBlank(path)){
            path = StrUtil.SLASH;
        }

        if(null == actionClass) {
            Logger.warn("Added blank action, pass it.");
            return;
        }
        //所有路径必须以 "/" 开头，如果没有则补全之
        if(false == path.startsWith(StrUtil.SLASH)) {
            path = StrUtil.SLASH + path;
        }
        String method = "GET";
        HttpMethod methodAnnotation = actionClass.getAnnotation(HttpMethod.class);
        if (methodAnnotation != null) {
            method = methodAnnotation.value();
        }
        ServerSetting.getActionMap(method).put(path, actionClass);
	}

	/**
	 * 增加Action类，已有的Action类将被覆盖<br>
	 * 所有Action都是以单例模式存在的！
	 * @param actionClass 带注解的Action类
	 */
	public static void setAction(Class<? extends Action> actionClass) {
        final Route route = actionClass.getAnnotation(Route.class);
        if(route != null){
            final String path = route.value();
            if(StrUtil.isNotBlank(path)){
                setAction(path, actionClass);
                return;
            }
        }
        throw new ServerSettingException("Can not find Route annotation,please add annotation to Action class!");
	}
	//----------------------------------------------------------------------------------------------- Action start
	
}
