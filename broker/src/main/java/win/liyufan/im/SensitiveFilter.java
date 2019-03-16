package win.liyufan.im;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SensitiveFilter {
    public enum MatchType {
        MIN_MATCH("最小匹配规则"),MAX_MATCH("最大匹配规则");

        String desc;
        MatchType(String desc) {
            this.desc = desc;
        }
    }

    private Map<Object,Object> sensitiveWordsMap;

    private static final String END_FLAG="end";

    public static void main(String[] args) {
        Set<String> sensitiveWords=new HashSet<>();
        sensitiveWords.add("你是傻逼");
        sensitiveWords.add("你是傻逼啊");
        sensitiveWords.add("你是坏蛋");
        sensitiveWords.add("你个大笨蛋");
        sensitiveWords.add("我去年买了个表");
        sensitiveWords.add("shit");

        SensitiveFilter textFilter=new SensitiveFilter(sensitiveWords);
        String text="你你你你是傻逼啊你，说你呢，你个大笨蛋。";
        System.out.println(textFilter.getSensitiveWords(text,MatchType.MAX_MATCH));
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(textFilter.getSensitiveWords(text,MatchType.MAX_MATCH));
                }
            }).start();
        }
        try {
            Thread.sleep(1000 * 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isContainSensitiveWord(String text){
        Set<String> sensitiveWords = getSensitiveWords(text, MatchType.MIN_MATCH);
        if(sensitiveWords!=null&&sensitiveWords.size()>0){
            return true;
        }
        return false;
    }

    public Set<String> getSensitiveWords(String text,MatchType matchType){
        Set<String> sensitiveWords=new HashSet<>();
        if(text==null||text.trim().length()==0){
            return sensitiveWords;
        }

        for(int i=0;i<text.length();i++){
            int sensitiveWordLength = getSensitiveWordLength(text, i, matchType);
            if(sensitiveWordLength>0){
                String sensitiveWord = text.substring(i, i + sensitiveWordLength);
                sensitiveWords.add(sensitiveWord);
                if(matchType==MatchType.MIN_MATCH){
                    break;
                }
                i=i+sensitiveWordLength-1;
            }
        }
        return sensitiveWords;
    }

    public int getSensitiveWordLength(String text,int startIndex,MatchType matchType){
        if(text==null||text.trim().length()==0){
            throw new IllegalArgumentException("The input text must not be empty.");
        }
        char currentChar;
        Map<Object,Object> currentMap=sensitiveWordsMap;
        int wordLength=0;
        boolean endFlag=false;
        for(int i=startIndex;i<text.length();i++){
            currentChar=text.charAt(i);
            Map<Object,Object> subMap=(Map<Object,Object>) currentMap.get(currentChar);
            if(subMap==null){
                break;
            }else {
                wordLength++;
                if(subMap.containsKey(END_FLAG)){
                    endFlag=true;
                    if(matchType==MatchType.MIN_MATCH){
                        break;
                    }else {
                        currentMap=subMap;
                    }
                }else {
                    currentMap=subMap;
                }
            }
        }
        if(!endFlag){
            wordLength=0;
        }
        return wordLength;
    }


    public SensitiveFilter(Set<String> sensitiveWords){
        sensitiveWordsMap=new ConcurrentHashMap<>(sensitiveWords.size());
        if(sensitiveWords==null||sensitiveWords.isEmpty()){
            //throw new IllegalArgumentException("Senditive words must not be empty!");
            return;
        }

        String currentWord;
        Map<Object,Object> currentMap;
        Map<Object,Object> subMap;
        Iterator<String> iterator = sensitiveWords.iterator();
        while (iterator.hasNext()){
            currentWord=iterator.next();
            if(currentWord==null||currentWord.trim().length()<2){  //敏感词长度必须大于等于2
                continue;
            }
            currentMap=sensitiveWordsMap;
            for(int i=0;i<currentWord.length();i++){
                char c=currentWord.charAt(i);
                subMap=(Map<Object, Object>) currentMap.get(c);
                if(subMap==null){
                    subMap=new HashMap<>();
                    currentMap.put(c,subMap);
                    currentMap=subMap;
                }else {
                    currentMap= subMap;
                }
                if(i==currentWord.length()-1){
                    //如果是最后一个字符，则put一个结束标志，这里只需要保存key就行了，value为null可以节省空间。
                    //如果不是最后一个字符，则不需要存这个结束标志，同样也是为了节省空间。
                    currentMap.put(END_FLAG,null);
                }
            }
        }
    }


}
