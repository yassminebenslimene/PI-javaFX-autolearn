const mongoose = require('mongoose');

const todo = mongoose.model('Todo', {
    title: {
        type: String,
    },
    priority: {
        type: Number,
    },
});

module.exports = todo;