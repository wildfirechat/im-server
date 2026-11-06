package cn.wildfirechat.sdk;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生成测试数据。本应用生成以下数据：
 * 1. 创建${totalUserCount}个用户
 * 2. 每个用户同其他${friendCount}个用户建立好友关系
 * 3. 确保每个人都加入${smallGroupCount}个群组成员数为${smallGroupMemberSize}的小群组
 * 4. 确保每个人都加入${middleGroupCount}个群组成员数为${middleGroupMemberSize}的中群组
 * 5. 确保每个人都加入${bigGroupCount}个群组成员数为${bigGroupMemberSize}的大群组
 * 6. 把数据分片${splitFileCount}个保存在文件中
 *
 * 好友关系和群组关系都是随机生成，有可能会出现最后几个用户无法满足精确好友数或者群组数的问题，但绝大部分数据都能满足需求。
 */
public class GenerateTestData {
    //IM服务Server API
    private static String AdminUrl = "http://10.206.0.5:18080";
    private static String AdminSecret = System.getenv("WILDFIRECHAT_ADMIN_SECRET_KEY");

    private static int totalUserCount = 20000;

    //每个用户的好友数
    private static int friendCount = 100;

    //每个用户拥有的小群数，和小群的大小。
    private static int smallGroupCount = 100;
    private static int smallGroupMemberSize = 20;

    //每个用户拥有的中群数，和中群的大小。
    private static int middleGroupCount = 100;
    private static int middleGroupMemberSize = 100;

    //每个用户拥有的大群数，和大群的大小。
    private static int bigGroupCount = 20;
    private static int bigGroupMemberSize = 1000;

    //数据文件分割成几个文件存储。
    private static int splitFileCount = 1;


    //统计添加好友数和创建群组数。
    private static AtomicInteger addFriendCount = new AtomicInteger(0);
    private static AtomicInteger createGroupCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        if (AdminSecret == null || AdminSecret.trim().isEmpty()) {
            throw new IllegalStateException("请将 WILDFIRECHAT_ADMIN_SECRET_KEY 设置为服务端配置的管理 API 密钥。");
        }
        AdminConfig.initAdmin(AdminUrl, AdminSecret);
        int addFriendRequestCount = friendCount*totalUserCount/2;
        int createSmallGroupCount = smallGroupCount * totalUserCount / smallGroupMemberSize;
        int createMiddleGroupCount = middleGroupCount * totalUserCount / middleGroupMemberSize;
        int createBigGroupCount = bigGroupCount * totalUserCount / bigGroupMemberSize;
        System.out.println("根据当前的参数，将创建 " + totalUserCount + " 名用户");
        System.out.println("添加大概 " + addFriendRequestCount + " 次好友请求");
        System.out.println("创建大概 " + createSmallGroupCount + " 个小型群组");
        System.out.println("创建大概 " + createMiddleGroupCount + " 个中等群组");
        System.out.println("创建大概 " + createBigGroupCount + " 个大型群组");
        System.out.println("等待10秒开始");
        Thread.sleep(10*1000);

        List<String> userIds = generateUserIds(totalUserCount);

        CountDownLatch latch = new CountDownLatch(5);

        new Thread(() -> {
            createUsers(userIds);
            latch.countDown();
        }).start();

        Map<String, Integer> unfulfilledFriendMap = new HashMap<>();
        new Thread(() -> {
            addFriends(userIds, friendCount, unfulfilledFriendMap);
            latch.countDown();
        }).start();

        Map<String, Set<String>> userSmallGroupMap = new HashMap<>();
        Map<String, Integer> unfulfilledSmallMap = new HashMap<>();
        new Thread(() ->{
            for (String userId : userIds) {
                createGroup(userId, userIds, userSmallGroupMap, smallGroupCount, smallGroupMemberSize, unfulfilledSmallMap);
            }
            latch.countDown();
        }).start();

        Map<String, Set<String>> userMiddleGroupMap = new HashMap<>();
        Map<String, Integer> unfulfilledMiddleMap = new HashMap<>();
        new Thread(() ->{
            for (String userId : userIds) {
                createGroup(userId, userIds, userMiddleGroupMap, middleGroupCount, middleGroupMemberSize, unfulfilledMiddleMap);
            }
            latch.countDown();
        }).start();

        Map<String, Set<String>> userBigGroupMap = new HashMap<>();
        Map<String, Integer> unfulfilledBigMap = new HashMap<>();
        new Thread(() ->{
            for (String userId : userIds) {
                createGroup(userId, userIds, userBigGroupMap, bigGroupCount, bigGroupMemberSize, unfulfilledBigMap);
            }
            latch.countDown();
        }).start();

        latch.await();

        System.out.println("数据初始化结束，实际建立好友关系: " + addFriendCount.get() + " 条。");
        if (!unfulfilledFriendMap.isEmpty()) {
            System.out.println("有部分用户没有建立" + friendCount + "个好友关系，分别是：");
            printUnfulfilledMap(unfulfilledFriendMap);
        }

        System.out.println("数据初始化结束，创建群组: " + createGroupCount.get() + " 个。");
        if (!unfulfilledSmallMap.isEmpty()) {
            System.out.println("有部分用户没有建立" + smallGroupMemberSize + "个群组成员的群，分别是：");
            printUnfulfilledMap(unfulfilledSmallMap);
        }
        if (!unfulfilledMiddleMap.isEmpty()) {
            System.out.println("有部分用户没有建立" + middleGroupMemberSize + "个群组成员的群，分别是：");
            printUnfulfilledMap(unfulfilledMiddleMap);
        }
        if (!unfulfilledBigMap.isEmpty()) {
            System.out.println("有部分用户没有建立" + bigGroupMemberSize + "个群组成员的群，分别是：");
            printUnfulfilledMap(unfulfilledBigMap);
        }


        System.out.println("保存数据到" + splitFileCount + "个文件中。");
        for (int i = 0; i < splitFileCount; i++) {
            String filePath = "data"+i+".csv"; // 文件路径
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            int partSize = totalUserCount/splitFileCount;
            for (int j = 0; j < partSize; j++) {
                String userId = userIds.get(i*partSize+j);
                writeDeviceInfo(writer, userId, 3);
                Set<String> smallGroupIds = userSmallGroupMap.get(userId);
                writeGroupIds(writer, smallGroupIds);
                Set<String> middleGroupIds = userMiddleGroupMap.get(userId);
                writeGroupIds(writer, middleGroupIds);
                Set<String> bigGroupIds = userBigGroupMap.get(userId);
                writeGroupIds(writer, bigGroupIds);
            }

            writer.close();
        }

        System.out.println("保存结束。");
    }

    private static void printUnfulfilledMap(Map<String, Integer> map) {
        if(map.isEmpty())
            return;

        map.forEach((s, integer) -> System.out.print(s + ":" + integer + " "));
        System.out.println();
    }

    private static void writeDeviceInfo(BufferedWriter writer, String userId, int platform) throws IOException {
        writer.write(userId);
        writer.write(",");
        writer.write(UUID.randomUUID().toString());
        writer.write(",");
        writer.write(""+platform);
        writer.write("\n");
    }

    private static void writeGroupIds(BufferedWriter writer, Set<String> groupIds) throws IOException {
        int writeCount = 0;
        for (String bigGroupId : groupIds) {
            writer.write(bigGroupId);
            writeCount++;
            if(writeCount < groupIds.size()) {
                writer.write(",");
            } else {
                writer.write("\n");
            }
        }
    }
    private static void createGroup(String userId, List<String> userIds, Map<String, Set<String>> userGroupMap, int groupCount, int memberSize, Map<String, Integer> unfulfilledUserMap) {
        Set<String> groupIds = userGroupMap.computeIfAbsent(userId, s -> new HashSet<>());
        int i = 0;
        while (groupIds.size() < groupCount){
            PojoGroupInfo groupInfo = new PojoGroupInfo();
            groupInfo.setTarget_id(UUID.randomUUID().toString());
            groupInfo.setOwner(userId);
            groupInfo.setName("Group_"+userId+i++);
            groupInfo.setType(2);
            groupIds.add(groupInfo.getTarget_id());

            List<String> memberCandidates = new ArrayList<>(userIds);
            List<String> memberIds = new ArrayList<>();
            memberIds.add(userId);

            while (!memberCandidates.isEmpty() && memberIds.size() < memberSize) {
                String memberId = memberCandidates.get((int)(Math.random()*memberCandidates.size()));
                memberCandidates.remove(memberId);
                if(memberId.equals(userId)) {
                    continue;
                }
                if (memberIds.contains(memberId)) {
                    continue;
                }
                Set<String> memberGroupIds = userGroupMap.computeIfAbsent(memberId, s -> new HashSet<>());
                if(memberGroupIds.contains(groupInfo.getTarget_id()) || memberGroupIds.size() >= groupCount) {
                    continue;
                }
                memberGroupIds.add(groupInfo.getTarget_id());
                memberIds.add(memberId);
            }

            List<PojoGroupMember> members = new ArrayList<>();
            for (String memberId : memberIds) {
                PojoGroupMember member1 = new PojoGroupMember();
                member1.setMember_id(memberId);
                members.add(member1);
            }

            try {
                IMResult<OutputCreateGroupResult> resultCreateGroup = GroupAdmin.createGroup(groupInfo.getOwner(), groupInfo, members, null, null, null);
                if (resultCreateGroup != null && resultCreateGroup.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    System.out.println("create group success");
                } else {
                    System.out.println("create group failure");
                    System.exit(-1);
                }
            } catch (Exception e) {
                System.out.println("create group failure:" + e);
                System.exit(-1);
            }

            createGroupCount.incrementAndGet();
            if(memberIds.size() < memberSize) {
                unfulfilledUserMap.put(userId + "'s group " + groupInfo.getTarget_id(), memberIds.size());
                System.out.println("Error, can not find enough user to create group for user:" + userId);
            }
        }
    }

    private static void addFriends(List<String> userIds, int count, Map<String, Integer> unfulfilledFriendMap) {
        ConcurrentLinkedQueue<Pair<String, String>> toAddSet = new ConcurrentLinkedQueue<>();
        Map<String, Set<String>> userFriends = new HashMap<>();
        for (String userId : userIds) {
            List<String> candidates = new ArrayList<>(userIds);
            Set<String> currentUserFriends = userFriends.computeIfAbsent(userId, s -> new HashSet<>());
            while (!candidates.isEmpty() && currentUserFriends.size() < count) {
                String friendId = candidates.get((int) (Math.random()*candidates.size()));
                candidates.remove(friendId);
                if(friendId.equals(userId)) {
                    continue;
                }

                if(!currentUserFriends.contains(friendId)) {
                    Set<String> targetUserFriends = userFriends.computeIfAbsent(friendId, s -> new HashSet<>());
                    if(targetUserFriends.size() < count) {
                        currentUserFriends.add(friendId);
                        targetUserFriends.add(userId);
                        toAddSet.add(new Pair<>(userId, friendId));
                    }
                }
            }
            if (currentUserFriends.size() < count) {
                unfulfilledFriendMap.put(userId, currentUserFriends.size());
                System.out.println("Error, can not find enough user to add friend for user:" + userId);
            }
        }
        int threadNum = 5;
        CountDownLatch latch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                while (true) {
                    Pair<String, String> pair = toAddSet.poll();
                    if(pair == null) {
                        latch.countDown();
                        break;
                    }
                    try {
                        IMResult<Void> result = RelationAdmin.setUserFriend(pair.first, pair.second, true, null);
                        if (result != null && (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || result.getErrorCode() == ErrorCode.ERROR_CODE_ALREADY_FRIENDS)) {
                            System.out.println("add friend success");
                            addFriendCount.incrementAndGet();
                        } else {
                            System.out.println("failure");
                            System.exit(-1);
                        }
                    } catch (Exception e) {
                        System.out.println("failure:" + e);
                        System.exit(-1);
                    }
                }
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("failure:" + e);
            System.exit(-1);
        }
    }

    private static String generateUserId(int index) {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static List<String> generateUserIds(int count) {
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            userIds.add(generateUserId(i));
        }
        return userIds;
    }

    private static void createUsers(List<String> userIds) {
        for (int i = 0; i < userIds.size(); i++) {
            InputOutputUserInfo userInfo = generateUserInfo(userIds.get(i), i);
            try {
                IMResult<OutputCreateUser> resultCreateUser = UserAdmin.createUser(userInfo);
                if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
                } else {
                    System.out.println("Create user failure");
                    System.exit(-1);
                }
            } catch (Exception e) {
                System.out.println("Create user failure");
                System.exit(-1);
            }
        }
    }

    private static InputOutputUserInfo generateUserInfo(String userId, int index) {
        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        //用户ID，必须保证唯一性
        userInfo.setUserId(userId);
        //用户名，一般是用户登录帐号，也必须保证唯一性。也就是说所有用户的userId必须不能重复，所有用户的name必须不能重复，但可以同一个用户的userId和name是同一个，一般建议userId使用一个uuid，name是"微信号"且可以修改，
        userInfo.setName(userId);
        userInfo.setMobile((11000000000L+index) + "");
        userInfo.setDisplayName("User"+index);

        return userInfo;
    }
    static class Pair<K, V> {
        K first;
        V second;

        public Pair() {
        }

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
