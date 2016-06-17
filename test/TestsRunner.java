import com.sbertech.testtask.InputParameters;
import com.sbertech.testtask.Main;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by tatianamalyutina on 16/06/16.
 */
public class TestsRunner {

    @Test
    public void measureScanExecutionTime(){
        long start = System.nanoTime();
        try {
            Main.main(new String[]{"/Users/tatianamalyutina/Books"});
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.nanoTime();
        System.out.println((end - start)/1000000 + " ms");

    }

    @Test(timeout=100)
    public void testScanExecutionTime() {
        try {
            Main.main(new String[]{"/Users/tatianamalyutina/Books", "-", "/Users/tatianamalyutina/Books/A Game Of Thrones ~ Books 1-5 EPUB,MOBI & PDF [GrYff0N]"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInputParametersCorrectness() {
        /**
         * Проверяем одновременное включение и исключение папок
         */
        InputParameters parameters = new InputParameters(new String[] {
                "\"\\\\epbyminsd0235\\Video Materials\"",
                "\"\\\\EPUALVISA0002.kyiv.com\\Workflow\\ORG\\Employees\\Special\"",
                "\"\\\\EPUALVISA0002.kyiv.com\\Workflow\\ORG\\Employees\\Lviv\"",
                "-",
                "\"\\\\epbyminsd0235\\Video Materials\"",
                "\"\\\\EPUALVISA0002.kyiv.com\\Workflow\\ORG\\Employees\\Special\"",
                "\"\\\\EPUALVISA0002.kyiv.com\\Workflow\\ORG\\Employees\\Lviv\""
        });
        assertEquals(3, parameters.getExcludedFolders().size());
        assertEquals(0, parameters.getIncludedFolders().size());

        InputParameters parameters1 = new InputParameters(new String[]{"/Users/tatianamalyutina/Books"});
        assertEquals(null, parameters1.getExcludedFolders());
        assertEquals(1, parameters1.getIncludedFolders().size());

        InputParameters parameters2 = new InputParameters(new String[]{"/Users/tatianamalyutina/Books", "-", "A Game Of Thrones ~ Books 1-5 EPUB,MOBI & PDF [GrYff0N]"});
        assertEquals(1, parameters2.getExcludedFolders());
        assertEquals(1, parameters2.getIncludedFolders().size());


    }


}
