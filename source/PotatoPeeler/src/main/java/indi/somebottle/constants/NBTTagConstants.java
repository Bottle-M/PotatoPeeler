package indi.somebottle.constants;

/**
 * 和 NBT TAG 相关的常量
 */
public class NBTTagConstants {
    /**
     * nbt 二进制数据中的 InhabitedTime 标签前缀二进制串的十六进制表示 <br>
     * 0x04 表示这是一个 Long 类型标签 <br>
     * 0x00 0x0D 表示这个标签的名字长度为 13 字节 <br>
     * 0x49 0x6E 0x68 0x61 0x62 0x69 0x74 0x65 0x64 0x54 0x69 0x6d 0x65 表示标签名 "InhabitedTime" <br>
     * 这段前缀之后的 8 个字节即为 InhabitedTime 的值（大端）
     * <br><br>
     * 参考: <a href="https://zh.minecraft.wiki/w/NBT%E6%A0%BC%E5%BC%8F?variant=zh-cn#%E4%BA%8C%E8%BF%9B%E5%88%B6%E6%A0%BC%E5%BC%8F">NBT 二进制格式</a>, <a href="https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9D%97%E6%A0%BC%E5%BC%8F">区块格式</a>
     */
    public static final byte[] INHABITED_TIME_TAG_BIN = {0x04, 0x00, 0x0D, 0x49, 0x6E, 0x68, 0x61, 0x62, 0x69, 0x74, 0x65, 0x64, 0x54, 0x69, 0x6d, 0x65};

    /*
        可以发现，INHABITED_TIME_TAG_BIN 序列无论以哪个下标为止，都没有任何的“相等前后缀”。

        用 KMP 算法来考虑的话，我在 nbt 文件的字节序列中寻找 INHABITED_TIME_TAG_BIN 时，只要遇到和主字节串失配的地方，就可以直接把 INHABITED_TIME_TAG_BIN 的搜索指针回退到头部，而主字节串指针不用回退。

        SomeBottle 2024.8.1
     */

    /**
     * 强制加载区块存储文件中的 Forced 标签前缀二进制串的十六进制表示 <br>
     * 0x0C 表示这是一个 Long 类型数组 <br>
     * 0x00 0x06 表示这个标签名字长 6 字节 <br>
     * 后面这一段表示标签名 Forced。<br>
     * 这段前缀后面 4 个字节是数组长度，随后则是 Long 类型的数组元素。
     */
    public static final byte[] FORCED_TAG_BIN = {0x0C, 0x00, 0x06, 0x46, 0x6F, 0x72, 0x63, 0x65, 0x64};

    /**
     * 数据版本标签前缀二进制串的十六进制表示 <br>
     * 0x03 表示这是一个 Int 类型标签 <br>
     * 0x00 0x0B 表示这个标签名字长 11 字节 <br>
     * 后面这一段表示标签名 DataVersion。<br>
     * 这段前缀后面 4 个字节是大端数据版本号
     */
    public static final byte[] DATA_VERSION_TAG_BIN = {0x03, 0x00, 0x0B, 0x44, 0x61, 0x74, 0x61, 0x56, 0x65, 0x72, 0x73, 0x69, 0x6F, 0x6E};

    /**
     * 区块标签数据前缀二进制串的十六进制表示 <br>
     * 0x09 表示这是一个 List 类型标签 <br>
     * 0x00 0x07 表示这个标签名字长 7 字节 <br>
     * 后面这一段表示标签名 tickets。<br>
     * 再后面 1 个字节是 List 中元素的类型，这里为复合标签 Compound - 0x0A <br>
     * 这段前缀后面 4 个字节表示 List 中元素的数量
     */
    public static final byte[] TICKETS_COMPOUND_TAG_BIN = {0x09, 0x00, 0x07, 0x74, 0x69, 0x63, 0x6B, 0x65, 0x74, 0x73, 0x0A};
}
