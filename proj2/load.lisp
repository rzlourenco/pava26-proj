(defmacro def-class (class &rest fields)
  (let ((classname (if (atom class) class (first class)))
        (superclasses (if (atom class) nil (rest class))))
  (labels
    ((make-name (&rest names)
       (intern (format nil "~{~a~}" names)))

     (make-keyword (&rest names)
       (intern (format nil "~{~a~}" names) "KEYWORD"))

     (get-fields (class)
       (let ((constructor-name (make-name "MAKE-" class)))
         (apply #'append
                (get constructor-name :fields)
                (mapcar #'get-fields (get constructor-name :super)))))

     (get-superclass-fields (superclasses)
       (mapcar
         (lambda (class) `(,class . ,(get-fields class)))
         superclasses))

     (get-all-fields (superclasses)
       (remove-duplicates
         (apply #'append
           fields
           (mapcar #'cdr
             (get-superclass-fields superclasses)))))

     (call-superclass-constructor (superclass params all-args)
       (let ((constructor-name (make-name "MAKE-" superclass))
             (args (intersection all-args params)))
         `(,constructor-name
            ,@(apply #'append
                     (mapcar
                       (lambda (arg) (list (make-keyword arg) arg))
                       args)))))
     (make-getter (field)
       (let ((getter-name (make-name classname "-" field)))
         `(defun ,getter-name (arg &optional newval)
            (when (or (not arg) (not (symbolp arg)))
              (return-from ,getter-name))
            (labels
              ((find-field (obj can-return)
                (let* ((fields (get obj :fields))
                       (field (assoc ',field fields)))
                  (cond
                    ((not obj) nil)
                    ((and field can-return) field)
                    (t (some #'identity (mapcar
                      (lambda (s)
                        (find-field
                          s
                          (or can-return (eq (get s :type) ',classname))))
                      (get obj :super))))))))
              (let ((field (find-field arg (eq (get arg :type) ',classname))))
              (if newval
                (setf (cdr field) newval)
                (cdr field)))))))
     (make-setter (field)
       (let ((setter-name (make-name classname "-" field)))
         `(defsetf ,setter-name ,setter-name))))
    (let* ((superclass-field-pairs (get-superclass-fields superclasses))
           (all-fields
             (remove-duplicates
               (apply #'append
                 fields
                 (mapcar #'cdr superclass-field-pairs))))
           (constructor-name (make-name "MAKE-" classname))
           (predicate-name (make-name classname "?")))
      `(progn
         ,@(mapcar #'make-getter all-fields)
         ,@(mapcar #'make-setter all-fields)

         (defun ,predicate-name (arg)
           (cond ((not arg) nil)
                 ((not (symbolp arg) ) nil)
                 ((eq (get arg :type) ',classname) t)
                 (t (some #'identity (mapcar #',predicate-name (get arg :super))))))

         (defun ,constructor-name (&key ,@all-fields)
           (let ((sym (gensym))
                 (super
                   ,(when
                      superclasses
                      `(list ,@(mapcar
                                 (lambda (pair)
                                   (call-superclass-constructor
                                     (car pair)
                                     (cdr pair)
                                     all-fields))
                                 superclass-field-pairs)))))
             (setf (get sym :super) super)
             (setf (get sym :type) ',classname)
             (setf (get sym :fields)
                   (list ,@(mapcar
                             (lambda (e) `(cons ',e ,e))
                             fields)))
             sym))

         (setf (get ',constructor-name :args) ',all-fields)
         (setf (get ',constructor-name :fields) ',fields)
         (setf (get ',constructor-name :super) ',superclasses)
         nil)))))
