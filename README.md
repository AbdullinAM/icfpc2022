# icfpc-2022

Team 301 entry for ICFPC 2022

## Participants

* Azat Abdullin
* Valentyn Sobol
* Daniil Stepanov
* Mikhail Belyaev

## The idea

We have used multiple approaches:
* Autocrop image borders as long as possible
* Cut the given block on some point and try to color all the sub blocks
* Randomly pick a point and try to find a bounding box around it with the same color
* Generate a random sequence of commands that improve score 


## Building the solution

`./gradlew build`

## Running the solution

```
./gradlew run --args "*problem number*"
```