# Math Object API

The `Math` object provides standard mathematical helper functions.

## Methods

### Math.sqrt(x)

Returns the square root of a number.

```javascript
Math.sqrt(16) // 4
```

### Math.abs(x)

Returns the absolute value of a number.

```javascript
Math.abs(-5) // 5
```

### Math.min(a, b)

Returns the smaller of two numbers.

```javascript
Math.min(10, 20) // 10
```

### Math.max(a, b)

Returns the larger of two numbers.

```javascript
Math.max(10, 20) // 20
```

### Math.floor(x)

Returns the largest integer less than or equal to a number.

```javascript
Math.floor(5.9) // 5
```

### Math.ceil(x)

Returns the smallest integer greater than or equal to a number.

```javascript
Math.ceil(5.1) // 6
```

### Math.round(x)

Returns the value of a number rounded to the nearest integer.

```javascript
Math.round(5.5) // 6
Math.round(5.4) // 5
```

### Math.random()

Returns a pseudo-random number between 0 (inclusive) and 1 (exclusive).

```javascript
// Random integer between 0 and 10
let r = Math.floor(Math.random() * 11)
```

### Math.pow(base, exponent)

Returns base to the exponent power.

```javascript
Math.pow(2, 3) // 8
```

## See Also

- [W2P Object](w2p-object.md)
- [Player Object](player-object.md)
