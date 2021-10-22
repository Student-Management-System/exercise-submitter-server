import java.util.function.Function;

public class Lambda {
    
    public static void main(String[] args) {
        Function<String, Integer> func = (str) -> str.length();
        System.out.println(func.apply("HelloWorld!"));
    }
    
}
