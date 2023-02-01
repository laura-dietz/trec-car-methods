package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * User: dietz
 * Date: 1/5/18
 * Time: 2:37 PM
 */
public class TrecCarReprUtils {
    @NotNull
    public static List<String> getEntitiesOnly(@NotNull Data.Paragraph p) {
        List<String> result = new ArrayList<>();
        for(Data.ParaBody body: p.getBodies()){
            if(body instanceof Data.ParaLink){
                result.add(((Data.ParaLink) body).getPage());
            }
        }
        return result;
    }

    @NotNull
    public static List<String> getEntityIdsOnly(@NotNull Data.Paragraph p) {
        List<String> result = new ArrayList<>();
        for(Data.ParaBody body: p.getBodies()){
            if(body instanceof Data.ParaLink){
                result.add(((Data.ParaLink) body).getPageId());
            }
        }
        return result;
    }

}
