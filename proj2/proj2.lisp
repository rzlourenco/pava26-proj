(defmacro def-class (className &rest fieldNames)
  (defun make-name (&rest names)
    (intern (format nil "~{~a~}" names)))

  (defun make-getter (field)
    (let ((getter-name (make-name className "-" field)))
      `(defun ,getter-name (arg)
         NIL)))

  (let ((constructor-name (make-name "MAKE-" className))
        (predicate-name (make-name className "?"))
        (predicates (mapcar #'make-getter fieldNames)))
    `(progn
       (defun ,constructor-name (&key ,@fieldNames)
         NIL)

       (defun ,predicate-name (arg)
         NIL)

       ,@predicates)))

(pprint
  (macroexpand `(def-class person name age)))

(def-class person
  name
  age)
