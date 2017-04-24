(defmacro def-class (className &rest fieldNames)
  (defun make-name (&rest names)
    (intern (format nil "~{~a~}" names)))

  (defun make-constructor ()
    (let ((constructor-name (make-name "MAKE-" className)))
      `(defun ,constructor-name ()
         nil)))

  (defun make-predicate ()
    (let ((predicate-name (make-name className "?")))
      `(defun ,predicate-name ()
         nil)))

  (defun make-getter (field)
    (let ((getter-name (make-name className "-" field)))
      `(defun ,getter-name (arg)
         nil)))

  (let ((constructor (make-constructor))
        (predicate (make-predicate))
        (getters (mapcar #'make-getter fieldNames)))

    `(progn
       ,constructor
       ,predicate
       ,@getters)))

(pprint
  (macroexpand `(def-class person name age)))

(def-class person
           name
           age)
