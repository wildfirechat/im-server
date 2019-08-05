package win.liyufan.im;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IDUtilsTest {
    @Rule public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void fromUidGoodCases() {
        Assert.assertEquals(623_272_613, IDUtils.fromUid("1a 2b 3c"));
    }

    @Test
    public void fromUidBadCase() {
        thrown.expect(StringIndexOutOfBoundsException.class);
        IDUtils.fromUid("foo");
        // No assertions: method throws an exception
    }
}
