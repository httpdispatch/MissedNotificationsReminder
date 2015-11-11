package com.app.missednotificationsreminder.binding.model;

import com.app.missednotificationsreminder.data.api.ApiService;
import com.app.missednotificationsreminder.data.api.model.MedicationsResponse;
import com.squareup.okhttp.ResponseBody;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit.Response;
import retrofit.Result;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

/**
 * Created by Eugene on 14.10.2015.
 */
public class MedicationListViewModelTest {
    @Inject
    ApplicationsSelectionViewModel model;

    @Before
    public void setUp() {
        ObjectGraph.create(new TestModule()).inject(this);
    }

    @Test
    public void testNormalFlow() {
        reset(model.view, model.apiService);

        stub(model.apiService.list(null, null))
                .toReturn(Observable.just(Result.response(Response.success(Mockito.mock(MedicationsResponse.class)))));

        model.loadData();

        verify(model.view).setLoadingState();
        verify(model.view, never()).setErrorState();

        verify(model.apiService).list(null, null);
        model.shutdown();
    }

    @Test
    public void testErrorFlow() {
        reset(model.view, model.apiService);
        stub(model.apiService.list(null, null))
                .toReturn(Observable.just(Result.response(Response.error(400, Mockito.mock(ResponseBody.class)))));
        model.loadData();

        verify(model.view).setLoadingState();
        verify(model.view).setErrorState();

        verify(model.apiService).list(null, null);
        model.shutdown();
    }

    @Module(
            injects = MedicationListViewModelTest.class
    )
    public static class TestModule {
        @Provides
        @Singleton
        @Named("main")
        Scheduler provideMainThreadScheduler() {
            return Schedulers.immediate();
        }

        @Provides
        @Singleton
        MedicationListView provideView() {
            MedicationListView result = Mockito.mock(MedicationListView.class);
            stub(result.getListLoadedAction()).toReturn(l -> Timber.d("getListLoadedAction"));
            return result;
        }

        @Provides
        @Singleton
        ApiService provideApi() {
            return Mockito.mock(ApiService.class);
        }
    }
}
