package nullProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class NullExamples {
    public static void main(String[] args) {
        //Initialization of nonNull and PossibleNull
        //nonNull and PossibleNull check
        /*@NotNull Point p1 = new Point(1,1);
        for (int i=1; i<5; i++) p1 = null;
        Point pN = null;
        if (p1 == null) System.out.println("Как эт@!?");
         else System.out.println("Всё чётк@!");
         if (pN == new Point(1,1)) System.out.println("Всё чётк@!");
         else System.out.println("Как эт@!?");*/
    }



    void foo(@NotNull Point p) {
    }
    void bar(Point p) {
    }
    void baz(@NotNull Point p1, @NotNull Point p2){
    }
    void sample(
            @NotNull Point uy,
            @NotNull Point nonNullP, // definitely not null
            Point defaultP, // may be null but we don't care
            @Nullable Point nullP // may be null and need checks
    ) {

        @NotNull Point examplePoint = new Point();
        if (examplePoint == null) // error: Redundant null check
            foo(examplePoint);


        if (nonNullP != null) // Condition is always true there
            if (nullP != null) { // no error
                int c = nullP.x; //no error as well
            }
        if (nonNullP != null) // Condition is always true there
            if (defaultP != null){ // no error
                int c = nullP.x; // error: nullP may be null
            }
        if (nonNullP != null) // Condition is always true there
            if (defaultP != null) { // no error
                int c = defaultP.x; // no error as well
            }


        if (nonNullP == null) {
            // ^^^^^^^ --- error: redundant null check
            int x = nonNullP.x; // no check needed
        }

        foo(nullP);
        // ^^^^^^ --- error: function foo requires not null parameter, but nullP may be null

        if (nullP != null) {
            foo(nullP); // no error: nullP is not null here
        }

        int z = nullP.x;
        //       ^^^^^^ --- error:  nullP may be null

        bar(nullP); // no error: bar parameter is not marked as @NotNull

        if (defaultP == null)
            baz(defaultP, nonNullP); // error: both parameters of baz must be @NotNull

        if (defaultP != null) {
            foo(defaultP); // no error: defaultP is not null here
        }

        int a = defaultP.x; // no error: defaultP may be null, but we don't care

        if (defaultP == null) {
            int b = defaultP.x;
            //      ^^^^^^^^^^ --- error: defaultP is always null here
        }
        bar(defaultP); // no error
        foo(defaultP);
        // ^^^^^^ --- error: function foo requires not null parameter, but defaultP may be null
    }

}
