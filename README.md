# NullAnalyzer
Цель проекта заключается в анализе входного файла на языке Java и его методов на наличие использования гарантированно null/not-null значений.
Для каждой ситуации описанной ниже в разделе  ***"What has been implemented in the project?"*** выводятся случаи ошибки использования гарантированных значений.

The basic purpose of the project is to analyze the input file in Java and its methods for the presence of using guaranteed null/not-null values.
For each situation described below in the section ***"What has been implemented in the project?"***, error cases of using guaranteed values are displayed.

# What has been implemented in the project?
На данный момент удалось реализовать и протестировать:
- [x] Поддержка аннотаций @Nullable / @NotNull:    
_(@Nullable / @NotNull annotations support)_
- [x] Сравнения гарантированно not-null/null значений с null, вложенные сравнения:   
_(Comparisons of guaranteed not-null/null values with null, nested "if"s)_

        if (nonNullP != null) 
            if (defaultP != null){ 
                int c = nullP.x; 
            }
- [x] Вызовы методов a.foo() с разным колличеством параметров методов и их аннотаций:  
_(Calls a.foo() methods with a different number of method parameters and their annotations)_

        void baz(@NotNull Point p1, @NotNull Point p2){}
        void foo(@NotNull Point p) {}
        
        if (defaultP == null)
            baz(defaultP, nonNullP);
        foo(nullP);
- [x] Обращения к полям:   
_(GettingFields)_

        int z = nullP.x;
        int x = nonNullP.x
- [x] Вывод в консоль типа ошибки с указанием переменной, а также строчки на которой она произошла:  
_(Output to the console of the error type indicating the variable, as well as the line on which it occurred)_
![image](https://user-images.githubusercontent.com/70843205/155975829-b192f35f-55c1-496b-90cc-67b92a521fea.png)
- [x] Различные их комбинации   
_(Their various combinations)_

# What this project includes?
В проекте содержатся:
- Основной класс с реализацией анализа файла
- Файл mainModule.jar
- Файл на языке Java (NullExamples.java), содержащий примеры использования null/not-null значений

# How to use it?
После сборки проекта в IDEA, убедитесь, что подаваемый аргумент содержит путь к .class файлу. Или используйте .jar файл.        
_(After building the project in IDEA, make sure that the program arguments contain the path to the .class file e.g, )_
![image](https://user-images.githubusercontent.com/70843205/155979532-da4c5191-4caf-467b-b12f-a01ff9386427.png)
