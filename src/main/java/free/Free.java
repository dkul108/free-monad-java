package free;

public abstract class Free<F, A>{
  private Free(){}

  public static <G, B> Free<G, B> done(final B b){
    return new Done<>(b);
  }

  public static <G, B> Free<G, B> liftF(final _1<G, B> value, final Functor<G> G){
    return new Suspend<>(G.map(Done::new, value));
  }

  public static <G, B> Free<G, B> suspend(final _1<G, Free<G, B>> b) {
    return new Suspend<>(b);
  }

  public final A go(final F1<_1<F, Free<F, A>>, Free<F, A>> f, final Functor<F> F){
    Free<F, A> current = this;
    while(true){
      final Either<_1<F, Free<F, A>>, A> either = current.resume(F);
      if(either.isLeft()){
        current = f.apply(either.leftOrNull());
      } else {
        return either.rightOrNull();
      }
    }
  }

  public final <G> _1<G, A> foldMap(final NT<F, G> f, final Functor<F> F, final Monad<G> G){
    return resume(F).<_1<G, A>>fold(
      left -> G.flatMap(x -> x.foldMap(f, F, G), f.apply(left)),
      right -> G.point(() -> right)
    );
  }

  /**
   * @param <X1> existential type
   * @param <X2> existential type
   */
  private static <X1, X2, F, A> Either<_1<F, Free<F, A>>, A> resume(Free<F, A> current, final Functor<F> F) {
    while(true) {
      if(current instanceof Done){
        return Either.right(current.asDone().a);
      }else if(current instanceof Suspend){
        return Either.left(current.asSuspend().a);
      }else {
        final Gosub<F, X1, A> gosub1 = current.asGosub();
        if(gosub1.a instanceof Done){
          current = gosub1.f.apply(gosub1.a.asDone().a);
        }else if(gosub1.a instanceof Suspend){
          return Either.left(F.map(o -> o.flatMap(gosub1.f), gosub1.a.asSuspend().a));
        }else {
          final Gosub<F, X2, X1> gosub2 = gosub1.a.asGosub();
          current = gosub2.a.flatMap(o ->
            gosub2.f.apply(o).flatMap(gosub1.f)
          );
        }
      }
    }
  }

  /**
   * @param <X> existential type
   */
  @SuppressWarnings("unchecked")
  private <X> Gosub<F, X, A> asGosub(){
    return (Gosub<F, X, A>)this;
  }

  private Suspend<F, A> asSuspend(){
    return (Suspend<F, A>)this;
  }

  private Done<F, A> asDone(){
    return (Done<F, A>)this;
  }

  public abstract <B> Free<F, B> flatMap(final F1<A, Free<F, B>> f);

  public final Either<_1<F, Free<F, A>>, A> resume(final Functor<F> F) {
    return resume(this, F);
  }

  public final <B> Free<F, B> map(final F1<A, B> f) {
    return flatMap(a -> new Done<>(f.apply(a)));
  }

  private static final class Done<F, A> extends Free<F, A>{
    private final A a;

    private Done(final A a) {
      this.a = a;
    }

    @Override
    public <B> Free<F, B> flatMap(F1<A, Free<F, B>> f) {
      return new Gosub<>(this, f);
    }
  }

  private static final class Suspend<F, A> extends Free<F, A>{
    private final _1<F, Free<F, A>> a;

    private Suspend(final _1<F, Free<F, A>> a) {
      this.a = a;
    }

    @Override
    public <B> Free<F, B> flatMap(F1<A, Free<F, B>> f) {
      return new Gosub<>(this, f);
    }
  }

  private static final class Gosub<F, A, B> extends Free<F, B>{
    private final Free<F, A> a;
    private final F1<A, Free<F, B>> f;

    private Gosub(final Free<F, A> a, final F1<A, Free<F, B>> f){
      this.a = a;
      this.f = f;
    }

    @Override
    public <C> Free<F, C> flatMap(final F1<B, Free<F, C>> g) {
      return new Gosub<>(a, aa -> new Gosub<>(f.apply(aa), g));
    }
  }

}

