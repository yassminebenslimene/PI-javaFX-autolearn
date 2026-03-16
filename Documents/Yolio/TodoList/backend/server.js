const express = require('express');
const app = express();
require('./config/connect');

const toDoRoute = require('./routes/todo')

app.use(express.json());


app.use('/todo', toDoRoute);

app.listen(3000, () => {
  console.log('Server is running on port 3000');
});