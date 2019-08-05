package win.liyufan.im;

import org.junit.Assert;
import org.junit.Test;

public class UtilityTest {
    @Test
    public void formatJsonInput() {
        Assert.assertEquals("", Utility.formatJson(null));
        Assert.assertEquals("1", Utility.formatJson("1"));
        Assert.assertEquals("{\n\t1", Utility.formatJson("{1"));
    }
}
